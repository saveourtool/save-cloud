package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.User
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository to access data about users
 */
@Repository
interface UserRepository : BaseEntityRepository<User> {
    /**
     * @param userName user name for update
     * @param rating new user rating
     * @return updated user
     */
    @Query(
        value = "update save_cloud.user u set u.rating = :rating where u.name = :user_name",
        nativeQuery = true,
    )
    fun updateUser(
        @Param("user_name") userName: String,
        @Param("rating") rating: Long,
    )

    /**
     * @param name name of organization
     * @return found [User] by name
     */
    @Query(
        value = "select * from save_cloud.user where name = :name",
        nativeQuery = true,
    )
    fun getUserByName(@Param("name") name: String): User
}
