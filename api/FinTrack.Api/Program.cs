using System.Security.Claims;
using System.Text;
using System.Threading.RateLimiting;
using FinTrack.Api.Data;
using FinTrack.Api.Models;
using FinTrack.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Npgsql;
using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi;

if (args.Contains("--healthcheck", StringComparer.Ordinal))
{
    using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
    try
    {
        using var response = await client.GetAsync("http://127.0.0.1:8080/health");
        Environment.ExitCode = response.IsSuccessStatusCode ? 0 : 1;
    }
    catch (HttpRequestException)
    {
        Environment.ExitCode = 1;
    }
    catch (TaskCanceledException)
    {
        Environment.ExitCode = 1;
    }
    return;
}

var builder = WebApplication.CreateBuilder(args);

var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
if (string.IsNullOrWhiteSpace(connectionString))
{
    var database = builder.Configuration.GetSection("Database");
    var host = database["Host"];
    var username = database["Username"];
    var password = database["Password"];
    if (string.IsNullOrWhiteSpace(host) || string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
        throw new InvalidOperationException("Configure the database host, username and password.");
    connectionString = new NpgsqlConnectionStringBuilder
    {
        Host = host,
        Database = database["Name"] ?? "fintrack",
        Username = username,
        Password = password,
        Port = database.GetValue("Port", 5432)
    }.ConnectionString;
}

builder.Services.AddDbContext<ApplicationDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddIdentityCore<ApplicationUser>(options =>
{
    options.User.RequireUniqueEmail = true;
    options.Password.RequiredLength = 12;
    options.Lockout.MaxFailedAccessAttempts = 5;
    options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
    options.Lockout.AllowedForNewUsers = true;
}).AddEntityFrameworkStores<ApplicationDbContext>();

if (args.Contains("--migrate", StringComparer.Ordinal))
{
    var migrationApp = builder.Build();
    await using var scope = migrationApp.Services.CreateAsyncScope();
    await scope.ServiceProvider.GetRequiredService<ApplicationDbContext>().Database.MigrateAsync();
    return;
}

var jwt = builder.Configuration.GetSection("Jwt");
var issuer = jwt["Issuer"];
var audience = jwt["Audience"];
var signingKey = jwt["SigningKey"];
if (string.IsNullOrWhiteSpace(issuer) || string.IsNullOrWhiteSpace(audience) ||
    string.IsNullOrWhiteSpace(signingKey) || Encoding.UTF8.GetByteCount(signingKey) < 32)
    throw new InvalidOperationException("Configure JWT issuer, audience and a signing key of at least 32 UTF-8 bytes.");

builder.Services.AddSingleton(new JwtSettings(issuer, audience, signingKey));
builder.Services.AddScoped<TokenService>();
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer(options =>
{
    options.MapInboundClaims = false;
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidIssuer = issuer,
        ValidateAudience = true,
        ValidAudience = audience,
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(signingKey)),
        ValidAlgorithms = [SecurityAlgorithms.HmacSha256],
        ValidateLifetime = true,
        RequireExpirationTime = true,
        ClockSkew = TimeSpan.Zero,
        NameClaimType = ClaimTypes.NameIdentifier
    };
});
builder.Services.AddAuthorization();
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.AddPolicy("auth", context =>
        RateLimitPartition.GetFixedWindowLimiter(context.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            _ => new FixedWindowRateLimiterOptions
            {
                PermitLimit = 10,
                Window = TimeSpan.FromMinutes(15),
                QueueLimit = 0,
                AutoReplenishment = true
            }));
});
builder.Services.AddProblemDetails();
builder.Services.AddControllers();
builder.Services.AddOpenApi(options =>
{
    options.AddDocumentTransformer((document, _, _) =>
    {
        document.Components ??= new OpenApiComponents();
        document.Components.SecuritySchemes ??= new Dictionary<string, IOpenApiSecurityScheme>();
        document.Components.SecuritySchemes["Bearer"] = new OpenApiSecurityScheme
        {
            Type = SecuritySchemeType.Http,
            Scheme = "bearer",
            BearerFormat = "JWT"
        };
        return Task.CompletedTask;
    });
    options.AddOperationTransformer((operation, context, _) =>
    {
        var metadata = context.Description.ActionDescriptor.EndpointMetadata;
        if (metadata.OfType<IAuthorizeData>().Any() && !metadata.OfType<IAllowAnonymous>().Any())
        {
            operation.Security ??= [];
            operation.Security.Add(new OpenApiSecurityRequirement
            {
                [new OpenApiSecuritySchemeReference("Bearer", context.Document)] = []
            });
        }
        return Task.CompletedTask;
    });
});

var app = builder.Build();
app.UseExceptionHandler();
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.UseSwaggerUI(options => options.SwaggerEndpoint("/openapi/v1.json", "FinTrack API v1"));
}

app.UseRouting();
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.MapGet("/health", async (ApplicationDbContext db, CancellationToken cancellationToken) =>
    await db.Database.CanConnectAsync(cancellationToken)
        ? Results.Ok(new { status = "healthy" })
        : Results.StatusCode(StatusCodes.Status503ServiceUnavailable));
app.Run();

public partial class Program;
