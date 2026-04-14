// Chart.js initialization for SubTrack analytics
(function() {
    'use strict';

    window.SubTrackCharts = {
        initMonthlyTrend: function(canvasId, labels, data) {
            var ctx = document.getElementById(canvasId);
            if (!ctx || typeof Chart === 'undefined') return;

            new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels || [],
                    datasets: [{
                        label: 'Monthly Spending',
                        data: data || [],
                        borderColor: '#4F46E5',
                        backgroundColor: 'rgba(79, 70, 229, 0.1)',
                        fill: true,
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return '$' + value;
                                }
                            }
                        }
                    }
                }
            });
        },

        initCategoryPie: function(canvasId, labels, data) {
            var ctx = document.getElementById(canvasId);
            if (!ctx || typeof Chart === 'undefined') return;

            new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: labels || [],
                    datasets: [{
                        data: data || [],
                        backgroundColor: [
                            '#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        },

        initSubscriptionBar: function(canvasId, labels, data) {
            var ctx = document.getElementById(canvasId);
            if (!ctx || typeof Chart === 'undefined') return;

            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels || [],
                    datasets: [{
                        label: 'Cost',
                        data: data || [],
                        backgroundColor: '#4F46E5'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        x: {
                            ticks: {
                                callback: function(value) {
                                    return '$' + value;
                                }
                            }
                        }
                    }
                }
            });
        }
    };
})();