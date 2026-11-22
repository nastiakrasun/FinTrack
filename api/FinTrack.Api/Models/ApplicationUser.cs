using Microsoft.AspNetCore.Identity;

namespace FinTrack.Api.Models;

public class ApplicationUser : IdentityUser<Guid>
{
    public string? DisplayName { get; set; }
    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
}
