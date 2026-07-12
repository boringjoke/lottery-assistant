ALTER TABLE lottery_prize_tiers
    DROP INDEX uk_draw_prize_group,
    ADD UNIQUE KEY uk_draw_prize_name (draw_id, prize_name);
