package com.joansala.    chess;

/*
 * Chess engine.
 * Copyright (C) 2015 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Cache;
import com.joansala.engine.Game;


/**
 * Implements a transposition table for a chess game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessCache implements Cache {


    /**
     * Creates a new empty transposition table with the specified
     * capacity in bytes.
     *
     * <p>The capacity of the table must be a power of two greater or
     * equal to 2 ^ 12. If size is not a power of two the table capacity
     * is set to the lowest near power of two.</p>
     *
     * @param capacity  The capacity of the table in bytes
     */
    protected ChessCache(int capacity) {
        /* TODO */
    }


    /**
     * Creates a new empty transposition with the given size.
     *
     * @param memory  Memory size request
     */
    public ChessCache(long memory) {
        /* TODO */
    }


    /**
     * {@inheritDoc}
     */
    public int getOutcome() {
        return 0; /* TODO */
    }


    /**
     * Returns the stored score value for the last position found.
     *
     * @return  Stored score value or zero
     */
    public int getScore() {
        return 0; /* TODO */
    }


    /**
     * Returns the stored move value for the last position found.
     *
     * @return  Stored move value or {@code Game.NULL_MOVE}
     */
    public int getMove() {
        return Game.NULL_MOVE; /* TODO */
    }


    /**
     * Returns the stored depth value for the last position found.
     *
     * @return  Stored depth value or zero
     */
    public int getDepth() {
        return 0; /* TODO */
    }


    /**
     * Returns the stored flag value for the last position found.
     *
     * @return  Stored flag value or {@code Cache.EMPTY}
     */
    public byte getFlag() {
        return Cache.EMPTY; /* TODO */
    }


    /**
     * Search a position provided by a {@code Game} object and sets it
     * as the current position on the transposition table.
     *
     * <p>When a position is found subsequent calls to the getter methods
     * of this object will return the values stored for the position.</p>
     *
     * @return  {@code true} if valid information for the position
     *          could be found; {@code false} otherwise.
     */
    public synchronized boolean find(Game game) {
        /* TODO */

        return false;
    }


    /**
     * Stores information about the current position in the {@code Game}
     * object on this transposition table.
     *
     * <p>For each parameter value only the lower bits are stored:
     * score (11 bits), depth (7 bits), flag (2 bits), move (4 bits),
     * game (44 bits, hash code of the current position).</p>
     *
     * <p>For the hash code of the position bits 44 to 12 (32 bits) are
     * stored explicitly on the corresponding table slot and the lower
     * 12 bits implicitly as part of the slot index.</p>
     *
     * @param game   The game for which the information about its current
     *               state must be stored
     * @param score  The evaluated score for the position
     * @param depth  The search depth with which the score was evaluated
     * @param flag   The score type as a lower bound, an upper bound,
     *               an exact value or empty.
     * @param move   The best move found so far for the position
     */
    public synchronized void store(Game game, int score, int move, int depth, byte flag) {
        /* TODO */
    }


    /**
     * Asks the cache to make room for new entries.
     *
     * <p>This implementation does not clear old entries immediately.
     * Instead, it increments an internal clock that marks stored
     * positions as old entries. Must be called periodically to make
     * room for new entries to be stored.</p>
     */
    public synchronized void discharge() {
        /* TODO */
    }


    /**
     * Resizes this transposition table clearing all the stored data.
     *
     * @param memory  The new memory request in bytes
     */
    public synchronized void resize(long memory) {
        /* TODO */
    }


    /**
     * Clears all the information stored in this transposition table.
     */
    public synchronized void clear() {
        /* TODO */
    }


    /**
     * Returns the current capacity of this cache in bytes.
     *
     * @return  Allocated bytes for the cache
     */
    public long size() {
        return 0; /* TODO */
    }
}
