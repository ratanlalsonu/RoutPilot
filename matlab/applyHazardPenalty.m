function penalty = applyHazardPenalty(roadStatus, weights)
% APPLYHAZARDPENALTY Maps road status (OPEN, WARNING, RESTRICTED, CRITICAL, BLOCKED) to routing penalty.
    switch upper(roadStatus)
        case 'OPEN'
            penalty = 0.0;
        case {'WARNING', 'RESTRICTED'}
            penalty = weights.warningPenalty;
        case 'CRITICAL'
            penalty = weights.criticalPenalty;
        case 'BLOCKED'
            penalty = inf;
        otherwise
            penalty = 0.0;
    end
end
