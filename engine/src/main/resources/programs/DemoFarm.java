import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.CropType;
import com.farmgame.sandbox.api.FarmApi;
import com.farmgame.sandbox.api.FarmProgram;
import com.farmgame.sandbox.api.RobotApi;

/**
 * Демо-программа игрока: сажает ряд культур, поливает, ждёт урожая и собирает его.
 *
 * Этот файл НЕ компилируется Gradle: игра компилирует его во время работы
 * через PlayerCodeCompiler — точно так же, как будет компилироваться код игрока.
 */
public class DemoFarm implements FarmProgram {

    private static final CropType[] ROTATION = {CropType.CARROT, CropType.WHEAT, CropType.CORN};

    @Override
    public void run(RobotApi robot, FarmApi farm) {
        int row = farm.getHeight() / 2;

        for (int season = 1; season <= 3; season++) {
            robot.say("Season " + season + ": planting");
            for (int x = 0; x < farm.getWidth(); x++) {
                robot.moveTo(x, row);
                Crop crop = Crop.builder()
                        .setType(ROTATION[x % ROTATION.length])
                        .setFertilized(season > 1)
                        .build();
                robot.plant(crop);
                robot.water();
            }

            robot.say("Waiting for the harvest...");
            while (!allRipe(farm, row)) {
                robot.pause(1.0);
            }

            int total = 0;
            for (int x = farm.getWidth() - 1; x >= 0; x--) {
                robot.moveTo(x, row);
                total += robot.harvest();
            }
            robot.say("Season " + season + " harvest: " + total);
        }
        robot.moveTo(0, 0);
        robot.say("Done! Corn in barn: " + farm.getHarvested(CropType.CORN));
    }

    private boolean allRipe(FarmApi farm, int row) {
        for (int x = 0; x < farm.getWidth(); x++) {
            if (!farm.isRipe(x, row)) {
                return false;
            }
        }
        return true;
    }
}
