package org.cqfn.save.backend.repository

import org.cqfn.save.entities.LnkUserProject
import org.cqfn.save.entities.Project
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository of [LnkUserProject]
 */
@Repository
interface LnkUserProjectRepository : BaseEntityRepository<LnkUserProject> {
    /**
     * @param project
     * @return [LnkUserProject] by [project]
     */
    fun findByProject(project: Project): List<LnkUserProject>

    /**
     * @param userId
     * @param project
     * @return [LnkUserProject] by [userId] and [Project]
     */
    fun findByUserIdAndProject(userId: Long, project: Project): LnkUserProject?

    /**
     * @param userId
     * @param projectId
     * @return [LnkUserProject] by [userId] and [projectId]
     */
    fun findByUserIdAndProjectId(userId: Long, projectId: Long): LnkUserProject?

    /**
     * Save [LnkUserProject] using only ids and role string.
     *
     * @param userId
     * @param projectId
     * @param role
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_user_project (project_id, user_id, role) values (:project_id, :user_id, :role)",
        nativeQuery = true,
    )
    fun save(
        @Param("project_id") projectId: Long,
        @Param("user_id") userId: Long,
        @Param("role") role: String
    )
}
