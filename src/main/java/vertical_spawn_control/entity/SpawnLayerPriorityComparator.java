package vertical_spawn_control.entity;

import java.util.Comparator;

public class SpawnLayerPriorityComparator implements Comparator<SpawnLayer> {

	@Override
	public int compare(SpawnLayer o1, SpawnLayer o2) {
		return o2.getPriority() - o1.getPriority();
	}

}
