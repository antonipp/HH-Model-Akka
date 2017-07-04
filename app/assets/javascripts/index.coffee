$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when "result"
        updateChart(message)
      else
        console.log(message)

getPricesFromArray = (data) ->
  (v[1] for v in data)

getChartArray = (data) ->
  ([i, v] for v, i in data)

getChartOptions = (data) ->
  series:
    shadowSize: 0
  yaxis:
    min: getAxisMin(data)
    max: getAxisMax(data)
  xaxis:
    min: 0
    max: 100

getAxisMin = (data) ->
  Math.min.apply(Math, data) * 0.9

getAxisMax = (data) ->
  Math.max.apply(Math, data) * 1.1

updateChart = (message) ->
  if ($("#" + message.sender).size() > 0)
    plot = $("#" + message.sender).data("plot")
    data = getPricesFromArray(plot.getData()[0].data)
    data.push(message.value)
    plot.setData([getChartArray(data)])
    # update the yaxes if either the min or max is now out of the acceptable range
    yaxes = plot.getOptions().yaxes[0]
    if ((getAxisMin(data) < yaxes.min) || (getAxisMax(data) > yaxes.max))
      # resetting yaxes
      yaxes.min = getAxisMin(data)
      yaxes.max = getAxisMax(data)
      plot.setupGrid()
    # redraw the chart
    plot.draw()
  else 
    $("#loading").hide()
    chart = $("<div>").addClass("chart").prop("id", message.sender)
    chartHolder = $("<div>").addClass("chart-holder").append(chart)
    container = $("<div>").addClass("chart-container").append(chartHolder)
    chartHolder.append($("<p>").text(message.sender))
    $("#charts").prepend(container)
    plot = chart.plot([getChartArray(message.value)], getChartOptions(message.value)).data("plot")
