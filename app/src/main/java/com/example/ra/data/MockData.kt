package com.example.ra.data

import com.example.ra.model.Car
import com.example.ra.model.FuelType
import com.example.ra.model.Host
import com.example.ra.model.Parish
import com.example.ra.model.TransmissionType

object MockData {

    val mockHosts = listOf(
        Host(
            id = "h1",
            name = "Dave Campbell",
            profilePicUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400",
            rating = 4.95,
            totalTrips = 42,
            isSuperhost = true,
            joinedYear = "2022"
        ),
        Host(
            id = "h2",
            name = "Shanice Bryan",
            profilePicUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400",
            rating = 4.88,
            totalTrips = 29,
            isSuperhost = true,
            joinedYear = "2023"
        ),
        Host(
            id = "h3",
            name = "Marcus Gordon",
            profilePicUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
            rating = 4.92,
            totalTrips = 56,
            isSuperhost = true,
            joinedYear = "2021"
        )
    )

    val mockCars = listOf(
        Car(
            id = "c1",
            make = "Toyota",
            model = "Axio",
            year = 2021,
            parish = Parish.KINGSTON,
            pricePerDayJmd = 12500.0,
            transmission = TransmissionType.AUTOMATIC,
            fuelType = FuelType.HYBRID,
            seats = 5,
            imageUrl = "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?auto=format&fit=crop&q=80&w=800",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?auto=format&fit=crop&q=80&w=800"
            ),
            host = mockHosts[0],
            rating = 4.9,
            tripCount = 34,
            features = listOf("Air Conditioning", "Bluetooth Audio", "Tinted Windows", "Backup Camera", "Keyless Entry"),
            description = "Immaculate Toyota Axio Hybrid located in Half Way Tree, Kingston. Extremely economic on gas, smooth drive, and perfect for getting around town or traveling coast to coast."
        ),
        Car(
            id = "c2",
            make = "Honda",
            model = "Vezel",
            year = 2022,
            parish = Parish.ST_JAMES,
            pricePerDayJmd = 16500.0,
            transmission = TransmissionType.AUTOMATIC,
            fuelType = FuelType.HYBRID,
            seats = 5,
            imageUrl = "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&q=80&w=800",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&q=80&w=800"
            ),
            host = mockHosts[1],
            rating = 4.96,
            tripCount = 28,
            features = listOf("Leather Interior", "Apple CarPlay", "Sunroof", "Drive Assist", "Extra Luggage Space"),
            description = "Stylish crossover SUV parked 10 mins from Sangster International Airport in Montego Bay. High ground clearance, cool AC, and spacious luggage area."
        ),
        Car(
            id = "c3",
            make = "Toyota",
            model = "Land Cruiser Prado",
            year = 2020,
            parish = Parish.ST_ANN,
            pricePerDayJmd = 32000.0,
            transmission = TransmissionType.AUTOMATIC,
            fuelType = FuelType.GASOLINE,
            seats = 7,
            imageUrl = "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&q=80&w=800",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&q=80&w=800"
            ),
            host = mockHosts[2],
            rating = 5.0,
            tripCount = 45,
            features = listOf("4WD Capability", "7 Seats", "Premium Sound", "Dual AC", "Off-Road Ready"),
            description = "Luxury 7-seater SUV in Ocho Rios. Perfect for family island tours, trips to Blue Mountain, or exploring the scenic north coast in complete comfort."
        ),
        Car(
            id = "c4",
            make = "Suzuki",
            model = "Swift",
            year = 2021,
            parish = Parish.WESTMORELAND,
            pricePerDayJmd = 10500.0,
            transmission = TransmissionType.AUTOMATIC,
            fuelType = FuelType.GASOLINE,
            seats = 5,
            imageUrl = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&q=80&w=800",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&q=80&w=800"
            ),
            host = mockHosts[0],
            rating = 4.85,
            tripCount = 19,
            features = listOf("Compact Parking", "Cool AC", "Bluetooth", "Gas Saver"),
            description = "Nimble and fun hatch parked in Negril near 7 Mile Beach. Easy to park, super economical, and great for cruising around town."
        ),
        Car(
            id = "c5",
            make = "Subaru",
            model = "Forester",
            year = 2021,
            parish = Parish.MANCHESTER,
            pricePerDayJmd = 18000.0,
            transmission = TransmissionType.AUTOMATIC,
            fuelType = FuelType.GASOLINE,
            seats = 5,
            imageUrl = "https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&q=80&w=800",
            galleryUrls = listOf(
                "https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&q=80&w=800"
            ),
            host = mockHosts[1],
            rating = 4.91,
            tripCount = 22,
            features = listOf("All-Wheel Drive", "Panoramic Roof", "Spacious Boot", "Bluetooth"),
            description = "Rugged and reliable AWD SUV in Mandeville. Handles mountain winding roads effortlessly."
        )
    )
}
