package hu.progmasters.servicebooker.domain.entity;

import hu.progmasters.servicebooker.domain.Period;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class SpecificPeriod implements Period {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime start;
    private LocalDateTime end;

    private String comment;

    @Enumerated(EnumType.STRING)
    private SpecificPeriodType type;

    @ManyToOne(optional = false)
    private Boose boose;

}
