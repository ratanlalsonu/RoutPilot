function comparison = compareAlgorithms(G, startNodeId, goalNodeId)
% COMPAREALGORITHMS Executes both A* and Dijkstra on the active road graph and compares metrics.
    aStarRes = calculateRouteAStar(G, startNodeId, goalNodeId);
    dijkRes = calculateRouteDijkstra(G, startNodeId, goalNodeId);

    comparison.aStar = aStarRes;
    comparison.dijkstra = dijkRes;
    comparison.nodeReductionRatio = 1.0 - (aStarRes.nodesEvaluated / max(1, dijkRes.nodesEvaluated));
    comparison.speedupMs = dijkRes.calculationTimeMs - aStarRes.calculationTimeMs;
end
