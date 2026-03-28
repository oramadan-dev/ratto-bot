package com.oramadan.ratto.ttrpg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ttrpg_event_player")
@IdClass(TtrpgEventPlayerId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TtrpgEventPlayerEntity {

    @Id
    @Column(nullable = false)
    private long eventId;

    @Id
    @Column(nullable = false)
    private long userId;
}
