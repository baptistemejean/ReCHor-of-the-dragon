package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

import ch.epfl.rechor.timetable.Platforms;
import ch.epfl.rechor.timetable.Routes;
import ch.epfl.rechor.timetable.StationAliases;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Transfers;

/**
 * Represents a public transport timetable whose (flattened) data are stored in files.
 * @param directory the path to the directory containing the timetable data files
 * @param stringTable the table of strings
 * @param stations the stations
 * @param stationAliases the alternative names of stations
 * @param platforms the platforms
 * @param routes the routes
 * @param transfers the transfers
 */
public record FileTimeTable(
        Path directory,
        List<String> stringTable,
        Stations stations,
        StationAliases stationAliases,
        Platforms platforms,
        Routes routes,
        Transfers transfers) implements TimeTable {

    /**
     * Creates a new FileTimeTable instance from the timetable data in the given directory.
     *
     * @param directory the path to the directory containing the timetable data
     * @return a new FileTimeTable instance
     * @throws IOException in case of I/O error
     */
    public static TimeTable in(Path directory) throws IOException {
        // Read the string table
        Path stringsPath = directory.resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        // Map the station data
        ByteBuffer stationsBuffer = mapFile(directory.resolve("stations.bin"));
        Stations stations = new BufferedStations(stringTable, stationsBuffer);

        // Map the station aliases data
        ByteBuffer stationAliasesBuffer = mapFile(directory.resolve("station-aliases.bin"));
        StationAliases stationAliases = new BufferedStationAliases(stringTable, stationAliasesBuffer);

        // Map the platforms data
        ByteBuffer platformsBuffer = mapFile(directory.resolve("platforms.bin"));
        Platforms platforms = new BufferedPlatforms(stringTable, platformsBuffer);

        // Map the routes data
        ByteBuffer routesBuffer = mapFile(directory.resolve("routes.bin"));
        Routes routes = new BufferedRoutes(stringTable, routesBuffer);

        // Map the transfers data
        ByteBuffer transfersBuffer = mapFile(directory.resolve("transfers.bin"));
        Transfers transfers = new BufferedTransfers(transfersBuffer);

        return new FileTimeTable(
                directory,
                stringTable,
                stations,
                stationAliases,
                platforms,
                routes,
                transfers);
    }

    private static ByteBuffer mapFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    @Override
    public Connections connectionsFor(LocalDate date) {
        Path connectionsPath = directory.resolve(date.toString()).resolve("connections.bin");
        Path connectionsSuccPath = directory.resolve(date.toString()).resolve("connections-succ.bin");

        try {
            ByteBuffer connectionsBuffer = mapFile(connectionsPath);
            ByteBuffer connectionsSuccBuffer = mapFile(connectionsSuccPath);
            return new BufferedConnections(connectionsBuffer, connectionsSuccBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Trips tripsFor(LocalDate date) {
        Path tripsPath = directory.resolve(date.toString()).resolve("trips.bin");

        try {
            ByteBuffer tripsBuffer = mapFile(tripsPath);
            return new BufferedTrips(stringTable, tripsBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
