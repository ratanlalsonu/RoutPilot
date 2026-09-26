function result = calculateRouteDijkstra(G, startNodeId, goalNodeId)
% CALCULATEROUTEDIJKSTRA Independent Dijkstra shortest-path implementation (no heuristic).
    tStart = tic;
    nodeIds = {G.nodes.id};
    n = length(nodeIds);
    startIdx = find(strcmp(nodeIds, startNodeId), 1);
    goalIdx = find(strcmp(nodeIds, goalNodeId), 1);

    dist = inf(1, n);
    prev = zeros(1, n);
    visited = false(1, n);

    dist(startIdx) = 0;
    nodesEvaluated = 0;

    for step = 1:n
        candidates = dist;
        candidates(visited) = inf;
        [minVal, u] = min(candidates);
        if isinf(minVal)
            break;
        end
        visited(u) = true;
        nodesEvaluated = nodesEvaluated + 1;

        if u == goalIdx
            break;
        end

        uId = nodeIds{u};
        for eIdx = 1:length(G.edges)
            edge = G.edges(eIdx);
            if ~edge.traversable
                continue;
            end
            vId = '';
            if strcmp(edge.from, uId)
                vId = edge.to;
            elseif strcmp(edge.to, uId)
                vId = edge.from;
            end
            if isempty(vId)
                continue;
            end
            v = find(strcmp(nodeIds, vId), 1);
            if visited(v)
                continue;
            end
            alt = dist(u) + edge.cost;
            if alt < dist(v)
                dist(v) = alt;
                prev(v) = u;
            end
        end
    end

    result.calculationTimeMs = toc(tStart) * 1000;
    result.nodesEvaluated = nodesEvaluated;
    result.totalCost = dist(goalIdx);
    p = {};
    curr = goalIdx;
    while curr ~= 0
        p = [{nodeIds{curr}}, p];
        if curr == startIdx, break; end
        curr = prev(curr);
    end
    result.path = p;
end
