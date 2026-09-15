package cl.duocuc.blackout.roster;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "rosters")
@Getter @Setter
public class Roster {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String game;
    @Column(length = 150)
    private String label;
    @Column(length = 20)
    private String number;
    @Column(length = 100)
    private String status;
    @ElementCollection
    @CollectionTable(name = "roster_players", joinColumns = @JoinColumn(name = "roster_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"roster_id", "player_id"}))
    @Column(name = "player_id", nullable = false)
    @OrderColumn(name = "position")
    private List<Long> playerIds = new ArrayList<>();
}
