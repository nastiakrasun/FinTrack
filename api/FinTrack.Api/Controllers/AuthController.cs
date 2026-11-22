using System.IdentityModel.Tokens.Jwt;
using FinTrack.Api.DTOs;
using FinTrack.Api.Models;
using FinTrack.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace FinTrack.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController(UserManager<ApplicationUser> users, TokenService tokens) : ControllerBase
{
    [HttpPost("register")]
    [EnableRateLimiting("auth")]
    public async Task<ActionResult<UserResponse>> Register(RegisterRequest request)
    {
        var email = request.Email.Trim();
        if (await users.FindByEmailAsync(email) is not null)
            return Conflict(new { error = "An account with this email already exists." });

        var user = new ApplicationUser
        {
            UserName = email,
            Email = email,
            DisplayName = request.DisplayName?.Trim()
        };
        IdentityResult result;
        try
        {
            result = await users.CreateAsync(user, request.Password);
        }
        catch (DbUpdateException ex) when (ex.InnerException is PostgresException
            { SqlState: PostgresErrorCodes.UniqueViolation, ConstraintName: "EmailIndex" or "UserNameIndex" })
        {
            return Conflict(new { error = "An account with this email already exists." });
        }

        if (!result.Succeeded)
        {
            if (result.Errors.Any(error => error.Code is "DuplicateEmail" or "DuplicateUserName"))
                return Conflict(new { error = "An account with this email already exists." });
            return BadRequest(new { errors = result.Errors.Select(error => error.Description) });
        }
        return StatusCode(StatusCodes.Status201Created, UserResponse.FromUser(user));
    }

    [HttpPost("login")]
    [EnableRateLimiting("auth")]
    public async Task<ActionResult<LoginResponse>> Login(LoginRequest request)
    {
        var user = await users.FindByEmailAsync(request.Email.Trim());
        if (user is null || await users.IsLockedOutAsync(user))
            return Unauthorized(new { error = "Invalid email or password." });
        if (!await users.CheckPasswordAsync(user, request.Password))
        {
            if (!(await users.AccessFailedAsync(user)).Succeeded)
                throw new InvalidOperationException("Failed to update the account lockout state.");
            return Unauthorized(new { error = "Invalid email or password." });
        }
        if (!(await users.ResetAccessFailedCountAsync(user)).Succeeded)
            throw new InvalidOperationException("Failed to update the account lockout state.");
        var (token, expires) = tokens.CreateToken(user);
        return Ok(new LoginResponse(token, expires, UserResponse.FromUser(user)));
    }

    [Authorize]
    [HttpGet("me")]
    public async Task<ActionResult<UserResponse>> Me()
    {
        if (!Guid.TryParse(User.FindFirst(JwtRegisteredClaimNames.Sub)?.Value, out var userId))
            return Unauthorized();
        var user = await users.FindByIdAsync(userId.ToString());
        return user is null ? Unauthorized() : Ok(UserResponse.FromUser(user));
    }
}
