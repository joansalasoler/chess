package com.joansala.game.chess.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  either version 3 of the License,  or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not,  see <http://www.gnu.org/licenses/>.
 */

import com.joansala.engine.Scorer;
import com.joansala.game.chess.ChessGame;
import static com.joansala.game.chess.Chess.*;
import static com.joansala.util.bits.Bits.*;


/**
 * This heuristic function evaluates the position of pieces on a
 * chessboard to estimate who has the advantage.
 *
 * It goes beyond just counting captured pieces (material advantage) and
 * considers where each piece is located on the board. Each square on the
 * chessboard has a different value depending on how good it is for a
 * particular piece to occupy that specific square.
 *
 * While it doesn't directly translate to features like "mobility",
 * "attack potential" or "defensive potential" it captures the essence
 * by considering each piece location and its inherent value.
 */
public final class PositionalScorer implements Scorer<ChessGame> {

    /**
     * {@inheritDoc}
     */
    public final int evaluate(ChessGame game) {
        int score = 0;

        final long white = game.state(WHITE);
        final long black = game.state(BLACK);

        for (int piece = KING; piece <= PAWN; piece++) {
            long south = white & game.state(piece);
            long north = black & game.state(piece);

            while (empty(south) == false) {
                int checker = first(south);
                score += WEIGHTS[piece][checker];
                south ^= bit(checker);
            }

            while (empty(north) == false) {
                int checker = first(north);
                score -= WEIGHTS[piece][checker ^ 56];
                north ^= bit(checker);
            }
        }

        return score;
    }


    /** Piece-square weights */
    private static final int[][] WEIGHTS = {{
          2,    3,   -3,    1,   -6,    1,    4,   -2,
          1,    1,   -2,   -5,   -7,   -1,    1,    0,
         -3,   -2,   -3,   -5,   -5,   -3,   -2,   -2,
         -6,   -4,   -5,   -5,   -4,   -3,    0,   -6,
         -4,   -2,   -3,   -3,   -3,   -1,   -2,   -2,
         -3,    3,    1,   -2,   -2,    0,    3,   -1,
         -3,   -4,    0,   -1,   -1,   -2,    0,    3,
          1,    0,   -4,   -6,   -2,    2,    3,   -7
    }, {
        111,  113,  114,  115,  118,  116,  115,  117,
        117,  117,  119,  118,  117,  118,  116,  113,
        118,  119,  117,  116,  117,  116,  117,  115,
        117,  117,  117,  117,  116,  116,  114,  116,
        117,  117,  119,  117,  115,  115,  114,  114,
        124,  122,  123,  120,  118,  118,  115,  116,
        123,  120,  124,  115,  117,  116,  113,  114,
        122,  122,  122,  124,  118,  120,  117,  114
    }, {
         51,   50,   55,   56,   56,   55,   53,   52,
         46,   54,   56,   54,   53,   52,   53,   49,
         51,   54,   54,   55,   53,   53,   52,   49,
         52,   55,   54,   55,   54,   53,   51,   50,
         52,   54,   58,   57,   57,   55,   53,   52,
         56,   61,   60,   56,   59,   57,   57,   54,
         59,   57,   62,   64,   62,   61,   58,   58,
         59,   58,   55,   62,   60,   58,   59,   58
    }, {
         39,   37,   40,   40,   39,   40,   41,   38,
         42,   45,   44,   42,   42,   43,   43,   42,
         43,   44,   45,   43,   43,   43,   43,   42,
         42,   43,   43,   46,   45,   43,   43,   41,
         41,   42,   46,   46,   47,   44,   42,   41,
         41,   46,   47,   46,   46,   47,   46,   40,
         36,   44,   48,   45,   40,   40,   43,   39,
         41,   42,   37,   39,   37,   32,   42,   38
    }, {
         36,   36,   35,   37,   35,   32,   36,   26,
         36,   37,   41,   38,   38,   37,   32,   35,
         37,   41,   40,   41,   40,   40,   37,   36,
         38,   41,   41,   42,   40,   40,   39,   37,
         41,   41,   46,   43,   45,   41,   40,   37,
         43,   47,   53,   48,   46,   43,   45,   33,
         37,   39,   46,   41,   43,   47,   34,   30,
         26,   37,   27,   45,   33,   35,   28,   19
    }, {
          9,    9,    9,    9,    9,    9,    9,    9,
          7,   14,   12,    8,    7,    7,    9,    5,
          8,   13,   10,   10,    8,    9,    9,    6,
          7,   11,   10,   11,   11,    9,    9,    6,
          7,   11,   11,   12,   12,   10,   11,    8,
          7,   12,   16,   17,   13,   12,   10,    9,
          8,   13,   24,   17,   20,   16,   25,   21,
          9,    9,    9,    9,    9,    9,    9,    9
    }};
}
