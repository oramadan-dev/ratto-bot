package com.oramadan.ratto.ttrpg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ttrpg_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TtrpgEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long guildId;

    @Column(nullable = false)
    private long gmUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false)
    private boolean recurringWeekly;
}
