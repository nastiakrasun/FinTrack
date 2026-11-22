using System.ComponentModel.DataAnnotations;
using FinTrack.Api.Models;

namespace FinTrack.Api.DTOs;

public sealed class RegisterRequest
{
    [Required, EmailAddress, StringLength(256)]
    public string Email { get; init; } = "";

    [Required, StringLength(128, MinimumLength = 12)]
    public string Password { get; init; } = "";

    [StringLength(100)]
    public string? DisplayName { get; init; }
}

public sealed class LoginRequest
{
    [Required, EmailAddress, StringLength(256)]
    public string Email { get; init; } = "";

    [Required, StringLength(128)]
    public string Password { get; init; } = "";
}

public sealed record UserResponse(Guid Id, string Email, string? DisplayName, DateTime CreatedAtUtc)
{
    public static UserResponse FromUser(ApplicationUser user) =>
        new(user.Id, user.Email!, user.DisplayName, user.CreatedAtUtc);
}

public sealed record LoginResponse(string AccessToken, DateTime ExpiresAtUtc, UserResponse User);
