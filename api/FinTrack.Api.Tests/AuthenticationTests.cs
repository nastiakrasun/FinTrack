using System.IdentityModel.Tokens.Jwt;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using FinTrack.Api.Data;
using FinTrack.Api.DTOs;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace FinTrack.Api.Tests;

public class AuthenticationTests : IDisposable
{
    private readonly Dictionary<string, string?> originalEnvironment = new();

    public AuthenticationTests()
    {
        SetEnvironment("ConnectionStrings__DefaultConnection",
            Environment.GetEnvironmentVariable("FINTRACK_TEST_CONNECTION")
                ?? throw new InvalidOperationException("Set FINTRACK_TEST_CONNECTION to a dedicated PostgreSQL test database."));
        SetEnvironment("Jwt__Issuer", "fintrack-tests");
        SetEnvironment("Jwt__Audience", "fintrack-tests");
        SetEnvironment("Jwt__SigningKey", "test-key-not-for-production-12345678901234567890");
    }

    private void SetEnvironment(string name, string value)
    {
        originalEnvironment[name] = Environment.GetEnvironmentVariable(name);
        Environment.SetEnvironmentVariable(name, value);
    }

    public void Dispose()
    {
        foreach (var (name, value) in originalEnvironment)
            Environment.SetEnvironmentVariable(name, value);
    }

    [Fact]
    public async Task Authentication_flow_uses_database_and_returns_only_safe_user_data()
    {
        await using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.UseEnvironment("Development"));
        await using (var scope = factory.Services.CreateAsyncScope())
            await scope.ServiceProvider.GetRequiredService<ApplicationDbContext>().Database.MigrateAsync();

        using var client = factory.CreateClient();
        var email = $"test-{Guid.NewGuid():N}@example.com";
        const string password = "Strong-Password123!";
        using var register = await client.PostAsJsonAsync("/api/auth/register",
            new RegisterRequest { Email = email, Password = password, DisplayName = "Test user" });
        Assert.Equal(HttpStatusCode.Created, register.StatusCode);
        var registerJson = await register.Content.ReadAsStringAsync();
        Assert.DoesNotContain("password", registerJson, StringComparison.OrdinalIgnoreCase);
        var created = await register.Content.ReadFromJsonAsync<UserResponse>();
        Assert.NotNull(created);

        using var duplicate = await client.PostAsJsonAsync("/api/auth/register",
            new RegisterRequest { Email = email.ToUpperInvariant(), Password = password });
        Assert.Equal(HttpStatusCode.Conflict, duplicate.StatusCode);

        using var wrong = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Email = email, Password = "Wrong-Password123!" });
        Assert.Equal(HttpStatusCode.Unauthorized, wrong.StatusCode);
        using var missing = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Email = "missing@example.com", Password = password });
        Assert.Equal(await wrong.Content.ReadAsStringAsync(), await missing.Content.ReadAsStringAsync());

        using var anonymous = await client.GetAsync("/api/auth/me");
        Assert.Equal(HttpStatusCode.Unauthorized, anonymous.StatusCode);

        using var login = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Email = email.ToUpperInvariant(), Password = password });
        Assert.Equal(HttpStatusCode.OK, login.StatusCode);
        var credentials = await login.Content.ReadFromJsonAsync<LoginResponse>();
        Assert.NotNull(credentials);
        var jwt = new JwtSecurityTokenHandler().ReadJwtToken(credentials.AccessToken);
        Assert.Equal(created.Id.ToString(), jwt.Subject);
        Assert.InRange(credentials.ExpiresAtUtc, DateTime.UtcNow.AddMinutes(14), DateTime.UtcNow.AddMinutes(16));

        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", credentials.AccessToken);
        using var me = await client.GetAsync("/api/auth/me");
        Assert.Equal(HttpStatusCode.OK, me.StatusCode);
        var current = await me.Content.ReadFromJsonAsync<UserResponse>();
        Assert.Equal(created, current);
        Assert.DoesNotContain("password", await me.Content.ReadAsStringAsync(), StringComparison.OrdinalIgnoreCase);

        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", credentials.AccessToken + "invalid");
        using var invalidToken = await client.GetAsync("/api/auth/me");
        Assert.Equal(HttpStatusCode.Unauthorized, invalidToken.StatusCode);
    }

    [Fact]
    public async Task Registration_rejects_invalid_input()
    {
        await using var factory = new WebApplicationFactory<Program>();
        using var client = factory.CreateClient();
        using var response = await client.PostAsJsonAsync("/api/auth/register",
            new RegisterRequest { Email = "not-an-email", Password = "short" });
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Login_locks_out_after_repeated_invalid_passwords()
    {
        await using var factory = new WebApplicationFactory<Program>();
        await using (var scope = factory.Services.CreateAsyncScope())
            await scope.ServiceProvider.GetRequiredService<ApplicationDbContext>().Database.MigrateAsync();
        using var client = factory.CreateClient();
        var email = $"test-{Guid.NewGuid():N}@example.com";
        const string password = "Strong-Password123!";
        using var registration = await client.PostAsJsonAsync("/api/auth/register",
            new RegisterRequest { Email = email, Password = password });
        Assert.Equal(HttpStatusCode.Created, registration.StatusCode);
        for (var i = 0; i < 5; i++)
        {
            using var wrong = await client.PostAsJsonAsync("/api/auth/login",
                new LoginRequest { Email = email, Password = "Wrong-Password123!" });
            Assert.Equal(HttpStatusCode.Unauthorized, wrong.StatusCode);
        }
        using var locked = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Email = email, Password = password });
        Assert.Equal(HttpStatusCode.Unauthorized, locked.StatusCode);
    }

    [Fact]
    public async Task Authentication_endpoints_limit_requests()
    {
        await using var factory = new WebApplicationFactory<Program>();
        using var client = factory.CreateClient();
        for (var i = 0; i < 10; i++)
        {
            using var response = await client.PostAsJsonAsync("/api/auth/register",
                new RegisterRequest { Email = "invalid", Password = "short" });
            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }
        using var limited = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Email = "invalid", Password = "short" });
        Assert.Equal(HttpStatusCode.TooManyRequests, limited.StatusCode);
    }

    [Fact]
    public async Task Swagger_ui_and_bearer_documentation_are_available_in_development()
    {
        await using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.UseEnvironment("Development"));
        using var client = factory.CreateClient();
        using var ui = await client.GetAsync("/swagger/index.html");
        Assert.Equal(HttpStatusCode.OK, ui.StatusCode);
        Assert.Contains("swagger-ui", await ui.Content.ReadAsStringAsync());
        using var configuration = await client.GetAsync("/swagger/index.js");
        Assert.Equal(HttpStatusCode.OK, configuration.StatusCode);
        Assert.Contains("/openapi/v1.json", await configuration.Content.ReadAsStringAsync());
        using var asset = await client.GetAsync("/swagger/swagger-ui-bundle.js");
        Assert.Equal(HttpStatusCode.OK, asset.StatusCode);

        using var document = await client.GetFromJsonAsync<JsonDocument>("/openapi/v1.json");
        Assert.NotNull(document);
        var root = document.RootElement;
        Assert.Equal("bearer", root.GetProperty("components").GetProperty("securitySchemes")
            .GetProperty("Bearer").GetProperty("scheme").GetString());
        var paths = root.GetProperty("paths");
        Assert.True(paths.GetProperty("/api/auth/me").GetProperty("get").TryGetProperty("security", out var security));
        Assert.True(security[0].TryGetProperty("Bearer", out _));
        Assert.False(paths.GetProperty("/api/auth/register").GetProperty("post").TryGetProperty("security", out _));
    }

    [Fact]
    public async Task Swagger_ui_and_openapi_are_not_available_in_production()
    {
        await using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.UseEnvironment("Production"));
        using var client = factory.CreateClient();
        using var ui = await client.GetAsync("/swagger/index.html");
        using var document = await client.GetAsync("/openapi/v1.json");
        Assert.Equal(HttpStatusCode.NotFound, ui.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, document.StatusCode);
    }
}
