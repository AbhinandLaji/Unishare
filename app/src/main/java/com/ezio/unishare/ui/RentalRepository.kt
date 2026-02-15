package com.ezio.unishare

interface RentalRepository {
    suspend fun getAllRentalItems(): List<RentalItem>
    suspend fun addRentalItem(item: RentalItem): Boolean
}
