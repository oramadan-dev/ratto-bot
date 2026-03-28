package com.oramadan.ratto.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currency")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyEntity {

    @Id
    private long userId;

    @Column
    private int chedda;

    public boolean hasChedda(int chedda) {
        return this.chedda >= chedda;
    }

    public void setChedda(int chedda) {
        if (chedda < 0) {
            throw new IllegalArgumentException("Currency [chedda] may not be less than 0.");
        }

        this.chedda = chedda;
    }

    public void addChedda(int chedda) {
        this.chedda += chedda;
    }

    public void removeChedda(int chedda) {
        if (this.chedda < chedda) {
            throw new IllegalArgumentException("Cannot remove more chedda than the currently user has.");
        }

        this.chedda -= chedda;
    }

}
