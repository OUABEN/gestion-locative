package com.gestionlocative.quittances.client;

import java.time.LocalDate;

import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;

public interface BienBailClient {

    Bien consulterBien(String id);

    BailPeriode consulterBail(
            String id,
            LocalDate debut,
            LocalDate fin
    );
}