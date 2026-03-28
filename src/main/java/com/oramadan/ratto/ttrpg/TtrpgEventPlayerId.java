package com.oramadan.ratto.ttrpg;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TtrpgEventPlayerId implements Serializable {

    private long eventId;
    private long userId;
}
