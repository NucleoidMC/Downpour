package io.github.haykam821.downpour.game.phase;

import io.github.haykam821.downpour.game.DownpourConfig;
import io.github.haykam821.downpour.game.map.DownpourMap;
import io.github.haykam821.downpour.game.map.DownpourMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class DownpourWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final DownpourMap map;
	private final DownpourConfig config;

	public DownpourWaitingPhase(GameSpace gameSpace, ServerWorld world, DownpourMap map, DownpourConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<DownpourConfig> context) {
		DownpourMapBuilder mapBuilder = new DownpourMapBuilder(context.config());
		DownpourMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			DownpourWaitingPhase phase = new DownpourWaitingPhase(activity.getGameSpace(), world, map, context.config());
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			DownpourActivePhase.setRules(activity);
			activity.deny(GameRuleType.PVP);

			// Listeners
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, DownpourActivePhase.getCenterSpawnPos(this.map)).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	private GameResult requestStart() {
		DownpourActivePhase.open(this.gameSpace, this.world, this.map, this.config);
		return GameResult.ok();
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		DownpourActivePhase.spawnAtCenter(this.world, this.map, player);
		return ActionResult.FAIL;
	}
}