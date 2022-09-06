package gg.bayes.challenge.persistence.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "dota_match")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchEntity {

    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "dota_match_sequence_generator"
    )
    @SequenceGenerator(
            name = "dota_match_sequence_generator",
            sequenceName = "dota_match_sequence",
            allocationSize = 1
    )
    @Id
    @Column(name = "id")
    private Long id;

    @OneToMany(mappedBy = "match", cascade = CascadeType.PERSIST)
    private Set<CombatLogEntryEntity> combatLogEntries = new HashSet<>();
}