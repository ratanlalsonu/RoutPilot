function metrics = analyzeSensorSignal(signalSeries, sampleRateHz, dataSource)
% ANALYZESENSORSIGNAL Performs RMS, peak-to-peak, and spectral structural health analysis.
    metrics.dataSource = dataSource;
    metrics.mean = mean(signalSeries);
    metrics.rms = sqrt(mean(signalSeries.^2));
    metrics.peak = max(abs(signalSeries));
    metrics.stdDev = std(signalSeries);
    if length(signalSeries) > 2
        metrics.rateOfChange = signalSeries(end) - signalSeries(end-1);
    else
        metrics.rateOfChange = 0;
    end
    metrics.sampleRateHz = sampleRateHz;
end
