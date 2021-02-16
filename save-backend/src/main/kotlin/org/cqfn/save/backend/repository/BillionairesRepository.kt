package org.cqfn.save.backend.repository

import org.cqfn.save.backend.entities.Billionaires
import org.springframework.data.jpa.repository.JpaRepository

interface BillionairesRepository : JpaRepository<Billionaires, Int>
