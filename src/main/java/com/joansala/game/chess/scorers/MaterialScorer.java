package com.joansala.game.chess.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Scorer;
import com.joansala.game.chess.ChessGame;
import static com.joansala.game.chess.Chess.*;
import static com.joansala.util.bits.Bits.*;


/**
 * Evaluate the current state using only a material balance heuristic.
 *
 * In chess, there's a basic idea called a "material advantage" that
 * helps players judge who's winning. This way of thinking about advantage
 * is like a simple scorecard. Each chess piece has a point value, like
 * pawns being worth 1 and queens being worth 9. This material heuristic
 * just adds up the point value of all the pieces the white player has
 * left, then subtracts the point value of the other player's pieces.
 * The bigger the difference, the bigger the advantage.
 */
public final class MaterialScorer implements Scorer<ChessGame> {

    /** Heuristic value of each piece */
    private static final int QUEEN_WEIGHT =   170;
    private static final int ROOK_WEIGHT =    108;
    private static final int BISHOP_WEIGHT =   66;
    private static final int KNIGHT_WEIGHT =   56;
    private static final int PAWN_WEIGHT =     30;


    /**
     * {@inheritDoc}
     */
    public final int evaluate(ChessGame game) {
        int score = 0;

        final long white = game.state(WHITE);
        final long black = game.state(BLACK);

        score += QUEEN_WEIGHT * count(white & game.state(QUEEN));
        score += ROOK_WEIGHT * count(white & game.state(ROOK));
        score += BISHOP_WEIGHT * count(white & game.state(BISHOP));
        score += KNIGHT_WEIGHT * count(white & game.state(KNIGHT));
        score += PAWN_WEIGHT * count(white & game.state(PAWN));

        score -= QUEEN_WEIGHT * count(black & game.state(QUEEN));
        score -= ROOK_WEIGHT * count(black & game.state(ROOK));
        score -= BISHOP_WEIGHT * count(black & game.state(BISHOP));
        score -= KNIGHT_WEIGHT * count(black & game.state(KNIGHT));
        score -= PAWN_WEIGHT * count(black & game.state(PAWN));

        return score;
    }
}
