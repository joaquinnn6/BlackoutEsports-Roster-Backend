package cl.duocuc.blackout.roster;
import java.util.List;
public record RosterResponse(Long id, String name, String game, String label, String number,
                             String status, List<Long> playerIds) {
    static RosterResponse from(Roster roster) {
        return new RosterResponse(roster.getId(), roster.getName(), roster.getGame(), roster.getLabel(),
            roster.getNumber(), roster.getStatus(), List.copyOf(roster.getPlayerIds()));
    }
}
