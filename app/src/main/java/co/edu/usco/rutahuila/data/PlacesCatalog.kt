package co.edu.usco.rutahuila.data


object PlacesCatalog {

    val places: List<Place> = listOf(
        Place(
            id = 1,
            name = "Desierto de la Tatacoa",
            municipality = "Villavieja",
            category = Category.NATURE,
            distanceKm = 38,
            emoji = "🌵",
            latitude = 3.2333,
            longitude = -75.1667,
            description = "Paisaje semiárido con dos zonas de colores muy distintos: el Cuzco, de tonos rojizos y ocres, y Los Hoyos, de tonos grises. Es famoso por sus laberintos de tierra, sus cactus y sus cielos despejados.",
            tip = "ve temprano en la mañana o al atardecer, a mediodía el calor es muy fuerte. Lleva mucha agua y gorra."
        ),
        Place(
            id = 2,
            name = "Observatorio Astronómico",
            municipality = "Villavieja",
            category = Category.CULTURE,
            distanceKm = 38,
            emoji = "🔭",
            latitude = 3.2290,
            longitude = -75.1690,
            description = "En medio de la Tatacoa, con muy poca contaminación lumínica, es uno de los mejores lugares de Colombia para ver estrellas, planetas y constelaciones con telescopio.",
            tip = "revisa el pronóstico del clima: una noche nublada arruina la observación."
        ),
        Place(
            id = 3,
            name = "Parque Arqueológico San Agustín",
            municipality = "San Agustín",
            category = Category.CULTURE,
            distanceKm = 227,
            emoji = "🗿",
            latitude = 1.8869,
            longitude = -76.2950,
            description = "Patrimonio de la Humanidad por la UNESCO. Reúne cientos de estatuas talladas en piedra por una cultura prehispánica, rodeadas de bosques y senderos.",
            tip = "contrata un guía local en la entrada: las historias de cada estatua hacen la visita mucho mejor."
        ),
        Place(
            id = 4,
            name = "Alto de los Ídolos",
            municipality = "San José de Isnos",
            category = Category.CULTURE,
            distanceKm = 220,
            emoji = "🏛️",
            latitude = 1.9321,
            longitude = -76.2347,
            description = "Parque arqueológico con tumbas y estatuas monumentales, algunas de las más grandes de la cultura agustiniana, en lo alto de una colina con vista a las montañas.",
            tip = "combínalo con el Salto de Bordones, que queda en la misma zona."
        ),
        Place(
            id = 5,
            name = "Estrecho del Magdalena",
            municipality = "San Agustín",
            category = Category.NATURE,
            distanceKm = 230,
            emoji = "🌊",
            latitude = 1.9350,
            longitude = -76.2530,
            description = "Punto donde el río Magdalena se encajona entre rocas y su cauce se reduce a solo un par de metros de ancho. Un espectáculo de fuerza natural.",
            tip = "no te acerques al borde de las rocas: son resbalosas y la corriente es muy peligrosa."
        ),
        Place(
            id = 6,
            name = "Salto de Bordones",
            municipality = "Saladoblanco",
            category = Category.NATURE,
            distanceKm = 245,
            emoji = "💧",
            latitude = 1.9547,
            longitude = -76.1985,
            description = "Una de las cascadas más altas de Colombia, que cae por un cañón cubierto de vegetación. Tiene miradores para apreciarla completa.",
            tip = "lleva chaqueta impermeable si vas a bajar al mirador inferior."
        ),
        Place(
            id = 7,
            name = "Rafting en el río Magdalena",
            municipality = "San Agustín",
            category = Category.ADVENTURE,
            distanceKm = 227,
            emoji = "🚣",
            latitude = 1.8833,
            longitude = -76.2667,
            description = "Recorridos en balsa por los rápidos del río Magdalena con instructores. Hay trayectos para principiantes y otros con rápidos más fuertes.",
            tip = "elige siempre un operador certificado y usa chaleco y casco en todo momento."
        ),
        Place(
            id = 8,
            name = "Represa de Betania",
            municipality = "Yaguará",
            category = Category.ADVENTURE,
            distanceKm = 50,
            emoji = "⛵",
            latitude = 2.6860,
            longitude = -75.4290,
            description = "Gran embalse ideal para paseos en lancha, kayak, pesca y deportes náuticos, rodeado de montañas. Muy popular los fines de semana.",
            tip = "prueba el pescado fresco en los restaurantes de la orilla."
        ),
        Place(
            id = 9,
            name = "Termales de Rivera",
            municipality = "Rivera",
            category = Category.NATURE,
            distanceKm = 25,
            emoji = "♨️",
            latitude = 2.7770,
            longitude = -75.2560,
            description = "Piscinas de aguas termales naturales a pocos minutos de Neiva, perfectas para descansar después de un día de paseo.",
            tip = "ve entre semana si quieres encontrarlo con menos gente."
        ),
        Place(
            id = 10,
            name = "Cueva de los Guácharos",
            municipality = "Acevedo",
            category = Category.ADVENTURE,
            distanceKm = 250,
            emoji = "🦇",
            latitude = 1.6200,
            longitude = -76.1000,
            description = "Parque Nacional Natural con senderos por bosque de niebla y cuevas habitadas por el guácharo, un ave nocturna. Fue el primer parque nacional de Colombia.",
            tip = "la caminata es exigente: usa botas, lleva linterna y revisa antes si el parque está abierto."
        ),
        Place(
            id = 11,
            name = "Malecón del río Magdalena",
            municipality = "Neiva",
            category = Category.CULTURE,
            distanceKm = 0,
            emoji = "🌅",
            latitude = 2.9300,
            longitude = -75.2950,
            description = "Paseo a orillas del río en el centro de Neiva, con zonas para caminar, comer y ver el atardecer sobre el Magdalena.",
            tip = "el atardecer desde aquí es de los mejores de la ciudad."
        ),
        Place(
            id = 12,
            name = "Monumento a La Gaitana",
            municipality = "Neiva",
            category = Category.CULTURE,
            distanceKm = 0,
            emoji = "🗡️",
            latitude = 2.9340,
            longitude = -75.2900,
            description = "Gran escultura a orillas del río Magdalena que rinde homenaje a La Gaitana, cacica indígena símbolo de la resistencia en el Huila.",
            tip = "visítalo junto con el Malecón, quedan muy cerca."
        ),
        Place(
            id = 13,
            name = "Caja de Agua de Paicol",
            municipality = "Paicol",
            category = Category.ADVENTURE,
            distanceKm = 120,
            emoji = "🪨",
            latitude = 2.4500,
            longitude = -75.7700,
            description = "Puente natural de roca bajo el cual pasa el río Páez, rodeado de pozos y formaciones rocosas. Ideal para caminar y tomar fotos.",
            tip = "usa zapatos con buen agarre, las rocas cerca del agua resbalan."
        )
    )
}
