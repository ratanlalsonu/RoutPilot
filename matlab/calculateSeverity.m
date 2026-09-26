function [severity, roadStatus] = calculateSeverity(reading, thresholds)
% CALCULATESEVERITY Classifies hazard into SAFE, WARNING, CRITICAL, or BLOCKED.

    levels = zeros(1, 5);
    levels(1) = classifySingle(reading.vibration, thresholds.vibration);
    levels(2) = classifySingle(abs(reading.tilt), thresholds.tilt);
    levels(3) = classifySingle(reading.strain, thresholds.strain);
    levels(4) = classifySingle(reading.displacement, thresholds.displacement);
    levels(5) = classifySingle(reading.waterLevel, thresholds.waterLevel);

    maxLevel = max(levels);
    numCritical = sum(levels >= 2);
    numWarning = sum(levels >= 1);

    if maxLevel == 3 || numCritical >= thresholds.combinedHazardRule.minCriticalSensorsForBlocked
        severity = 'BLOCKED';
        roadStatus = 'BLOCKED';
    elseif maxLevel == 2 || numWarning >= thresholds.combinedHazardRule.minWarningSensorsForCritical
        severity = 'CRITICAL';
        roadStatus = 'BLOCKED';
    elseif maxLevel == 1
        severity = 'WARNING';
        roadStatus = 'RESTRICTED';
    else
        severity = 'SAFE';
        roadStatus = 'OPEN';
    end
end

function lvl = classifySingle(val, t)
    if val >= t.blocked
        lvl = 3;
    elseif val >= t.critical
        lvl = 2;
    elseif val >= t.warning
        lvl = 1;
    else
        lvl = 0;
    end
end
