package com.joansala.chess;

/*
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

import java.io.File;
import java.io.IOException;

import com.joansala.engine.Negamax;
import com.joansala.engine.UCIService;


/**
 * Universal Chess Interface service for chess.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessService {

    /** Default size of the hash table in megabytes */
    public static final int DEFAULT_HASH = 32;

    /** The default score to which draws are evaluated */
    public final static int DEFAULT_CONTEMPT = -9; /* TODO */


    /**
     * This class cannot be instantiated.
     */
    private ChessService() { }


    /**
     * Reads commands from the standard input and writes replies to the
     * standard output.
     *
     * @param argv  Command line arguments
     */
    public static void main(String[] argv) {
        // Opening book initialization

        ChessRoots roots = null;

        try {
            String bookPath = ChessService.class
                .getResource("/chess-book.bin").getFile();
            roots = new ChessRoots(new File(bookPath));
        } catch (Exception e) {
            System.err.println("Warning: Unable to open book file");
        }

        // Endgames book initialization

        ChessLeaves leaves = null;

        try {
            String leavesPath = ChessService.class
                .getResource("/chess-leaves.bin").getFile();
            leaves = new ChessLeaves(new File(leavesPath));
        } catch (Exception e) {
            System.err.println("Warning: Unable to open endgames file");
        }

        // Initialize game objects

        ChessCache cache = new ChessCache(DEFAULT_HASH << 20);
        ChessBoard board = new ChessBoard();
        ChessGame game = new ChessGame();

        // Engine initialization

        Negamax engine = new Negamax();

        engine.setContempt(DEFAULT_CONTEMPT);
        engine.setInfinity(ChessGame.MAX_SCORE);
        engine.setLeaves(leaves);
        engine.setCache(cache);

        // Service initialization

        UCIService service = new UCIService(board, game, engine);

        service.setContempt(DEFAULT_CONTEMPT);
        service.setRoots(roots);
        service.setCache(cache);

        // Start the communication

        service.start();
    }
}
