package cl.duocuc.blackout.roster;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/rosters")
public class RosterController {
    private final RosterService service;
    public RosterController(RosterService service) { this.service = service; }
    @GetMapping
    public List<RosterResponse> list() { return service.list(); }
    @GetMapping("/{id}")
    public RosterResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<RosterResponse> create(@Valid @RequestBody RosterRequest request) {
        var saved = service.create(request);
        return ResponseEntity.created(URI.create("/api/rosters/" + saved.id())).body(saved);
    }
    @PutMapping("/{id}")
    public RosterResponse update(@PathVariable Long id, @Valid @RequestBody RosterRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
