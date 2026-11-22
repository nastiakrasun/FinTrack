using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using FinTrack.Api.Models;
using Microsoft.IdentityModel.Tokens;

namespace FinTrack.Api.Services;

public sealed record JwtSettings(string Issuer, string Audience, string SigningKey);

public sealed class TokenService(JwtSettings settings)
{
    public (string Token, DateTime ExpiresAtUtc) CreateToken(ApplicationUser user)
    {
        var now = DateTime.UtcNow;
        var expires = now.AddMinutes(15);
        var token = new JwtSecurityToken(
            issuer: settings.Issuer,
            audience: settings.Audience,
            claims: [new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString())],
            notBefore: now,
            expires: expires,
            signingCredentials: new SigningCredentials(
                new SymmetricSecurityKey(Encoding.UTF8.GetBytes(settings.SigningKey)),
                SecurityAlgorithms.HmacSha256));
        return (new JwtSecurityTokenHandler().WriteToken(token), expires);
    }
}
