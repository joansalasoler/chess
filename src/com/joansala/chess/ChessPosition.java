package com.joansala.chess;

/*
 * Copyright (C) 2015-2016 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static com.joansala.chess.Chess.*;


/**
 * Represents a position in a chess game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessPosition implements Cloneable {

    /** Castling rights */
    byte castle = 0;

    /** En passant square */
    byte passant = 0;

    /** Half-moves clock */
    int clock = 0;

    /** Full-moves counter */
    int counter = 0;

    /** Position representation */
    long[] bitboards = new long[15];


    /**
     * Intantiates a new position representing the default start position
     * for a chess game.
     */
    public ChessPosition() {
        System.arraycopy(START_BITBOARDS, 0, bitboards, 0, 15);
        this.castle = START_CASTLE;
        this.passant = NULL_PASSANT;
        this.clock = 0;
        this.counter = 1;
    }


    /**
     * Returns true if this is a valid chess position representation.
     *
     * @return      {@code true} if this position is valid
     */
    public boolean isValid() {
        long bits, wbits, bbits, abits;

        // Validate data structures and counters

        if ((castle & ~START_CASTLE) != 0) {
            log("Invalid castle rights");
            return false;
        }

        if (passant < 0 || passant > 63) {
            if (passant != NULL_PASSANT) {
                log("Invalid en-passant");
                return false;
            }
        }

        if (clock < 0 || counter < 1) {
            log("Invalid clock or counter");
            return false;
        }

        if (bitboards == null || bitboards.length != 15) {
            log("Invalid bitboard structure");
            return false;
        }

        // There must be exactly one white king

        bits = bitboards[WHITE_KING];

        if (bits == 0 || (bits & (bits - 1)) != 0) {
            log("Invalid white king");
            return false;
        }

        // There must be exactly one black king

        bits = bitboards[BLACK_KING];

        if (bits == 0 || (bits & (bits - 1)) != 0) {
            log("Invalid black king");
            return false;
        }

        // A king cannot check another king

        wbits = bitboards[WHITE_KING];
        bbits = bitboards[BLACK_KING];
        bits = wbits | bbits;

        if ((bits & 0x8080808080808080L) == 0 ||
            (bits & 0x0101010101010101L) == 0) {
            if (wbits > bbits ^ wbits < 0 ^ bbits < 0) {
                if (((wbits >>> 1) / bbits & 0x1C1L) != 0) {
                    log("Invalid white king checker");
                    return false;
                }
            } else {
                if (((bbits >>> 1) / wbits & 0x1C1L) != 0) {
                    log("Invalid black king checker");
                    return false;
                }
            }
        }

        // No pawns on the first and last rows

        bits = 0xFF000000000000FFL;
        abits = bitboards[WHITE_PAWN] | bitboards[BLACK_PAWN];

        if ((bits & abits) != 0) {
            log("Invalid pawn checker");
            return false;
        }

        // Check for overlapping pieces

        bits = bitboards[WHITE_KING];

        for (int piece = 1; piece < 12; piece++) {
            if ((bits & bitboards[piece]) != 0) {
                log("Overlapping pieces");
                return false;
            }
            bits |= bitboards[piece];
        }

        // Check bitboards coherence

        if (bits != bitboards[ALL_PIECES]) {
            log("Missing pieces");
            return false;
        }

        wbits = bitboards[WHITE_PIECES];
        bbits = bitboards[BLACK_PIECES];

        if ((wbits | bbits) != bitboards[ALL_PIECES]) {
            log("Missing player pieces");
            return false;
        }

        if ((wbits & bbits) != 0) {
            log("Overlapping player pieces");
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        ChessPosition p = (ChessPosition) o;

        return java.util.Arrays.equals(this.bitboards, p.bitboards) &&
               this.castle == p.castle &&
               this.passant == p.passant;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChessPosition clone() throws CloneNotSupportedException {
        return (ChessPosition) super.clone();
    }


    private void log(String message) {
        if (DEBUG) System.err.println(message);
    }
}
