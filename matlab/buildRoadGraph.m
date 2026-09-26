function G = buildRoadGraph(nodes, edges, roadStatuses, vehicleType, weights)
% BUILDROADGRAPH Constructs directed road network graph with dynamic road status & vehicle constraints.

    G.nodes = nodes;
    G.edges = [];

    for i = 1:length(edges)
        e = edges(i);
        status = 'OPEN';
        if isfield(roadStatuses, e.id)
            status = roadStatuses.(e.id);
        end
        isCompatible = applyVehicleRestrictions(e, vehicleType);
        if strcmp(status, 'BLOCKED') || ~isCompatible
            e.traversable = false;
            e.cost = inf;
        else
            e.traversable = true;
            hazardPen = applyHazardPenalty(status, weights);
            e.cost = calculateRouteCost(e.distanceKm, e.travelTimeMin, e.trafficFactor, hazardPen, 0, weights);
        end
        G.edges = [G.edges, e];
    end
end
