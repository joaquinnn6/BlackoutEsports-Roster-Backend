package cl.duocuc.blackout.roster;
import jakarta.validation.constraints.*;
import java.util.List;
public record RosterRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String game,
    @Size(max = 150) String label,
    @Size(max = 20) String number,
    @Size(max = 100) String status,
    @NotNull @Size(max = 50) List<@NotNull @Positive Long> playerIds
) { }
