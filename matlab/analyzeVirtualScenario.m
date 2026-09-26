function report = analyzeVirtualScenario(scenarioName, timeSteps, thresholds)
% ANALYZEVIRTUALSCENARIO Evaluates a controlled Virtual Mode test trajectory (VIRTUAL_TEST source).
    report.scenarioName = scenarioName;
    report.dataSource = 'VIRTUAL_TEST';
    report.label = 'TEST DATA — NOT LIVE';
    report.totalSteps = length(timeSteps);
    report.warningStep = -1;
    report.criticalStep = -1;

    for k = 1:length(timeSteps)
        stepReading = timeSteps(k);
        stepReading.dataSource = 'VIRTUAL_TEST';
        [sev, ~] = calculateSeverity(stepReading, thresholds);
        if strcmp(sev, 'WARNING') && report.warningStep == -1
            report.warningStep = k;
        elseif (strcmp(sev, 'CRITICAL') || strcmp(sev, 'BLOCKED')) && report.criticalStep == -1
            report.criticalStep = k;
        end
    end
end
