package com.patatos.sac.anoca.cards.data

import android.arch.persistence.room.*

@Dao
interface MyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCards(vararg cards: DataCard): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategories(vararg categories: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeights(weights: List<Weight>)

    @Update
    fun updateCards(vararg cards: DataCard)

    @Update
    fun updateCategories(vararg categories: Category)

    @Update
    fun updateWeights(vararg weights: Weight)

    @Delete
    fun deleteCards(vararg cards: DataCard)

    @Query("DELETE FROM Cards WHERE category_id = :categoryId")
    fun deleteCardsFromCategory(categoryId: Long)

    @Delete
    fun deleteCategories(vararg categories: Category)

    @Delete
    fun deleteWeights(weights: List<Weight>)

    //@Unused
    @Query("SELECT Cards.* FROM Cards JOIN Categories JOIN Weights WHERE Cards.category_id = Categories.id AND Categories.enabled AND Cards.id = Weights.card_id ORDER BY Weights.last_time ASC")
    fun allCards(): List<DataCard>

    @Query("SELECT * FROM Cards WHERE category_id = :categoryId")
    fun allCards(categoryId: Long): List<DataCard>

    @Query("SELECT * FROM Weights ORDER BY last_time ASC")
    fun allWeights(): List<Weight>

    // @see [here](images.use.perl.org/use.perl.org/_bart/journal/33630.html) for weighted random selection -> ORDER BY -LOG(1.0 - RANDOM()) / weight
    // BCK: @Query("SELECT Cards.* FROM Cards JOIN Categories JOIN Weights WHERE Cards.category_id = Categories.id AND Categories.enabled AND Cards.id = Weights.card_id ORDER BY Weights.last_time + RANDOM() * (3 * 60 * 60 * 1000) ASC LIMIT 1")
    @Query("SELECT Cards.* FROM Cards JOIN Categories JOIN Weights WHERE Cards.category_id = Categories.id AND Categories.enabled AND Cards.id = Weights.card_id ORDER BY Weights.last_time + RANDOM() * (3 * 60 * 60 * 1000) ASC LIMIT 2")
    fun randomCard(): List<DataCard>

    @Query("SELECT Cards.* FROM Cards JOIN Categories JOIN Weights WHERE Cards.category_id = :categoryIdRestriction AND Cards.category_id = Categories.id AND Categories.enabled AND Cards.id = Weights.card_id ORDER BY Weights.last_time + RANDOM() * (3 * 60 * 60 * 1000) ASC LIMIT 2")
    fun randomCardCategoryRestriction(categoryIdRestriction: Long): List<DataCard>

    @Query("SELECT DISTINCT Cards.* FROM Cards JOIN Weights WHERE Cards.id != :cardId AND Cards.category_id = :categoryId AND Cards.id = Weights.card_id ORDER BY Weights.last_time + RANDOM() * (5 * 60 * 60 * 1000) ASC LIMIT :limit")
    fun randomCards(cardId: Long, categoryId: Long, limit: Int = 1): List<DataCard>

    @Query("SELECT * FROM Cards WHERE category_id = :categoryId AND can_included ORDER BY RANDOM() LIMIT :limit")
    fun includeCard(categoryId: Long, limit: Int = 1): List<DataCard>

    //@Unused
    @Query("SELECT * FROM Cards WHERE data_f LIKE :search OR data_b LIKE :search AND category_id = :categoryId")
    fun findCard(search: String, categoryId: Long): List<DataCard>

    @Query("SELECT * FROM Categories ORDER BY name")
    fun allCategories(): List<Category>

    @Query("SELECT * FROM Categories WHERE id = :categoryId")
    fun getCategory(categoryId: Long): Category

    @Query("SELECT * FROM Categories WHERE name LIKE :name")
    fun findCategories(name: String): List<Category>

    @Query("SELECT * FROM Weights WHERE card_id = :cardId ORDER BY last_time ASC LIMIT :limit")
    fun firstWeights(cardId: Long, limit: Int = 1): List<Weight>


    /// spacial import / export 'csv' function (for DataCard::class, Category::class and Weight::class)
    @Query("SELECT * FROM Cards")
    fun getCards(): List<DataCard>

    @Query("SELECT * FROM Categories")
    fun getCategories(): List<Category>

    @Query("SELECT * FROM Weights")
    fun getWeights(): List<Weight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCards(a: List<DataCard>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCategories(a: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setWeights(a: List<Weight>)

    @Query("DELETE FROM Cards WHERE 1")
    fun clearCards()

    @Query("DELETE FROM Categories WHERE 1")
    fun clearCategories()

    @Query("DELETE FROM Weights WHERE 1")
    fun clearWeights()
}
