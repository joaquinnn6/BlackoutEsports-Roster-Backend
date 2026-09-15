package cl.duocuc.blackout.roster;
import java.util.HashSet;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
@Transactional(readOnly = true)
public class RosterService {
    private final RosterRepository repository;
    public RosterService(RosterRepository repository) { this.repository = repository; }
    public List<RosterResponse> list() {
        return repository.findAll(Sort.by("id")).stream().map(RosterResponse::from).toList();
    }
    public RosterResponse get(Long id) { return RosterResponse.from(find(id)); }
    @Transactional
    public RosterResponse create(RosterRequest request) {
        Roster roster = new Roster();
        apply(roster, request);
        return RosterResponse.from(repository.saveAndFlush(roster));
    }
    @Transactional
    public RosterResponse update(Long id, RosterRequest request) {
        Roster roster = find(id);
        apply(roster, request);
        return RosterResponse.from(repository.saveAndFlush(roster));
    }
    @Transactional
    public void delete(Long id) { repository.delete(find(id)); }
    private Roster find(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Roster no encontrado"));
    }
    private void apply(Roster roster, RosterRequest request) {
        if (new HashSet<>(request.playerIds()).size() != request.playerIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un jugador en el roster");
        roster.setName(request.name().trim());
        roster.setGame(request.game().trim());
        roster.setLabel(request.label());
        roster.setNumber(request.number());
        roster.setStatus(request.status());
        // Los IDs son referencias externas; este servicio no comparte la base de jugadores.
        // Vaciar y hacer flush evita colisiones únicas al reordenar dos jugadores.
        if (roster.getId() != null) {
            roster.getPlayerIds().clear();
            repository.flush();
        }
        roster.getPlayerIds().clear();
        roster.getPlayerIds().addAll(request.playerIds());
    }
}
