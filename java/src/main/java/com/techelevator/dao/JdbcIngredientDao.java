package com.techelevator.dao;

import com.techelevator.model.Ingredient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcIngredientDao implements IngredientDao{
    private JdbcTemplate jdbcTemplate;
    public JdbcIngredientDao (JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // getting ALL recipes in database regardless of what the specific user added
    @Override
    public List<Ingredient> getAllIngredients() {
        List<Ingredient> ingredients = new ArrayList<>();

        String sql = "SELECT * FROM ingredients;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

        while(rowSet.next()) {
            ingredients.add(mapRowSetToIngredient(rowSet));
        }
        return ingredients;
    }

    // get ingredients for a recipe
    public List<Ingredient> getIngredientsForRecipe(int recipeId) {
        List<Ingredient> recipeIngredients = new ArrayList<>();

        String sql = "SELECT ingredient_id, ingredient_name, ingredient_type "
                        + "FROM ingredients JOIN recipe_ingredients ri ON ri.recipe_id = ingredients.ingredient_id "
                        + "WHERE recipe_id = ?;";

        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, recipeId);

        while(row.next()) {
            recipeIngredients.add(mapRowSetToIngredient(row));
        }
        return recipeIngredients;
    }

    /* add ingredient to ingredients table (for accumulated list).
        Also add to users_ingredient to track who added it (so that if they update/delete, we can allow only the users that
        added it to delete it)
    */
    @Override
    public void addIngredient(Ingredient ingredientToAdd, int userId) {
        String addIngredients = "INSERT INTO ingredients (ingredient_name, ingredient_type) VALUES (?, ?, ?);";
        int newIngredientId = jdbcTemplate.queryForObject(addIngredients, int.class, ingredientToAdd.getIngredientName(), ingredientToAdd.getIngredientType(),
                            ingredientToAdd.getIngredientId());

        String addUserIngredient = "INSERT INTO users_ingredients (user_id, ingredient_id) VALUES(?, ?);";
        jdbcTemplate.update(addUserIngredient, userId, newIngredientId);
    }

    // add ingredients to recipes (linking them in recipe_ingredients)
    // can track what users select/type in for ingredients section (do a for loop in front end and call this method for each ingredient)
    // when making a new recipe and store that info here
    @Override
    public void addIngredientToRecipe(Ingredient ingredient, int recipeId, BigDecimal amount) {
        String sql = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, amount) "
                    + "VALUES (?, ?, ?);";

        jdbcTemplate.update(sql, recipeId, ingredient.getIngredientId(), amount);
    }


    // deleting an ingredient from recipe (modifying)
    @Override
    public void deleteIngredientForRecipe(Ingredient ingredient, int recipeId, int userId) {
        // confirm that it's the user's recipe
        String sql = "SELECT * FROM users_recipes WHERE user_id=? AND recipe_id=?;";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, userId, recipeId);

        // if we have a row returned (meaning the recipe is stored as the user's specific recipe), then delete
        if (row.next()) {
            String deleteSql = "DELETE FROM recipe_ingredients WHERE ingredient_id =? AND recipe_id=?;";
            jdbcTemplate.update(deleteSql, ingredient.getIngredientId(), recipeId);
        }
    }

    //TODO: deleting an ingredient from the database?
    // Not sure if this would be necessary though..
    // it would require removing foreign keys from multiple tables


    private Ingredient mapRowSetToIngredient(SqlRowSet row) {
        Ingredient ingredient = new Ingredient();

        ingredient.setIngredientId(row.getInt("ingredient_id"));
        ingredient.setIngredientName(row.getString("ingredient_name"));
        ingredient.setIngredientType(row.getString("ingredient_type"));

        return ingredient;
    }
}
