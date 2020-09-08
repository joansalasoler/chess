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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.joansala.engine.Book;
import com.joansala.engine.Game;
import com.joansala.engine.Leaves;

import static com.joansala.engine.Game.*;
import static com.joansala.chess.Chess.*;


/**
 * Implements an endgame database for chess.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessLeaves extends Book implements Leaves {

    /** Header signature for the book format */
    public static final String SIGNATURE = "Chess Endgames ";

    /** Last found position score */
    private int score = Game.DRAW_SCORE;


    /**
     * Instantiates a new endgames book object.
     *
     * @param file      book file
     * @param seeds     maximum number of seeds
     */
    public ChessLeaves(File file) throws IOException {
        super(file, SIGNATURE);
    }


    /**
     * Returns the exact score value for the last position found
     * from south's perspective.
     *
     * @return  the stored score value or zero
     */
    public int getScore() {
        return this.score;
    }


    /**
     * Search a position provided by a {@code OwareGame} object and sets
     * it as the current position on the endgames book.
     *
     * @param game  A game object
     * @return      {@code true} if an exact score for the position
     *              could be found; {@code false} otherwise
     */
    public boolean find(Game g) {
        final ChessGame game = (ChessGame) g;

        /* TODO */

        return false;
    }
}
