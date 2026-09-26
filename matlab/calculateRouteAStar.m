function result = calculateRouteAStar(G, startNodeId, goalNodeId)
% CALCULATEROUTEASTAR Real A* shortest-path algorithm: f(n) = g(n) + h(n)
    tStart = tic;
    nodeIds = {G.nodes.id};
    n = length(nodeIds);
    startIdx = find(strcmp(nodeIds, startNodeId), 1);
    goalIdx = find(strcmp(nodeIds, goalNodeId), 1);

    gScore = inf(1, n);
    fScore = inf(1, n);
    cameFrom = zeros(1, n);
    visited = false(1, n);
    openSet = false(1, n);

    gScore(startIdx) = 0;
    fScore(startIdx) = haversineKm(G.nodes(startIdx), G.nodes(goalIdx));
    openSet(startIdx) = true;
    nodesEvaluated = 0;

    while any(openSet)
        candidates = fScore;
        candidates(~openSet) = inf;
        [~, current] = min(candidates);

        if current == goalIdx
            break;
        end

        openSet(current) = false;
        visited(current) = true;
        nodesEvaluated = nodesEvaluated + 1;

        currId = nodeIds{current};
        for eIdx = 1:length(G.edges)
            edge = G.edges(eIdx);
            if ~edge.traversable
                continue;
            end
            neighborId = '';
            if strcmp(edge.from, currId)
                neighborId = edge.to;
            elseif strcmp(edge.to, currId)
                neighborId = edge.from;
            end
            if isempty(neighborId)
                continue;
            end
            nbIdx = find(strcmp(nodeIds, neighborId), 1);
            if visited(nbIdx)
                continue;
            end
            tentativeG = gScore(current) + edge.cost;
            if tentativeG < gScore(nbIdx)
                cameFrom(nbIdx) = current;
                gScore(nbIdx) = tentativeG;
                fScore(nbIdx) = tentativeG + haversineKm(G.nodes(nbIdx), G.nodes(goalIdx));
                openSet(nbIdx) = true;
            end
        end
    end

    result.calculationTimeMs = toc(tStart) * 1000;
    result.nodesEvaluated = nodesEvaluated;
    result.totalCost = gScore(goalIdx);
    result.path = reconstructPath(cameFrom, nodeIds, startIdx, goalIdx);
end

function d = haversineKm(n1, n2)
    R = 6371.0;
    dLat = deg2rad(n2.lat - n1.lat);
    dLon = deg2rad(n2.lon - n1.lon);
    a = sin(dLat/2)^2 + cos(deg2rad(n1.lat)) * cos(deg2rad(n2.lat)) * sin(dLon/2)^2;
    d = 2 * R * atan2(sqrt(a), sqrt(1-a));
end

function p = reconstructPath(cameFrom, nodeIds, s, g)
    p = {};
    curr = g;
    while curr ~= 0
        p = [{nodeIds{curr}}, p];
        if curr == s, break; end
        curr = cameFrom(curr);
    end
end
