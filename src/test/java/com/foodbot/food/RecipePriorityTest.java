package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipePriorityTest {

    private Food food(String id, Long owner, Long creator) {
        return new Food(id, id, 10, "MainCourse", List.of("x"), owner, creator, null, Lang.EN);
    }

    @Test
    void ownFavoriteAndFollowedComeFirstPublicLast() {
        Food publicFood = food("pub", null, null);
        Food publicByStranger = food("pub2", null, 99L);
        Food mine = food("mine", 1L, 1L);
        Food fav = food("fav", null, 99L);
        Food followed = food("followed", null, 7L);

        List<Food> result = RecipePriority.publicLast(
                List.of(publicFood, publicByStranger, mine, fav, followed), 1L, Set.of("fav"), Set.of(7L));

        assertEquals(List.of(mine, fav, followed, publicFood, publicByStranger), result);
    }
}
