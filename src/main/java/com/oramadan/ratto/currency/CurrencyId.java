package com.oramadan.ratto.currency;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CurrencyId implements Serializable {

    private long guildId;
    private long userId;
}
