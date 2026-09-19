# openHAB Heating Optimizer Add-on

This openHAB add-on contains algorithms intended for scheduling heating based on electricity spot prices.

## Installation

Download the latest [release](https://github.com/pappnu/openhab-heating-optimizer/releases) JAR and place it to your openHAB installation's [addons directory](https://www.openhab.org/docs/installation/linux.html#file-locations).

## Actions

### Continuous period optimization

Continuous period optimization algorithm finds the cheapest continuous heating period for the current day, and the next day if spot prices reach that far. It checks each possible period of the configured length, adds up its spot prices, and selects the period with the lowest total price.

The following settings can be configured in the UI:

- `Persistence service`: ID of the persistence service to use for fetching and storing data.
- `Spot prices item`: Item containing the spot prices.
- `Length of the period to search for in hours`
- `Result item`: Item where the optimization result is stored to.

### Heating optimization

Heating optimization uses a mixed-integer linear program based approach to find optimal heating periods.

The following settings can be configured in the UI:

- `Persistence service`: ID of the persistence service to use for fetching and storing data.
- `Spot prices item`: Item containing the spot prices.
- `Air temperatures item`: Item containing the air temperatures used to determine heating demand.
- `Ascending average temperature levels for heating needs`: Temperature thresholds for the heating-demand curve, e.g. _-20.0,0.5,15_.
- `Heating hours required at each temperature level`: Required heating hours at each configured temperature level, e.g. _24.0,10,0.0_.
- `Ascending average temperature levels for max heating gaps`: Temperature thresholds for the maximum allowed non-heating gaps, e.g. _-20.0,0.5,15_.
- `The hours heating may be continuously off at each temperature level`: Maximum allowed non-heating duration at each configured temperature level, e.g. _1.0,4.5,24.0_.
- `Ascending average temperature levels for max starts`: Temperature thresholds for the maximum number of heating starts, e.g. _-20.0,0.5,15_.
- `The number of times heating may start during a day at each temperature level`: Maximum number of heating starts per day at each configured temperature level, e.g. _6,4,3_.
- `Minimum length of a heating period in hours`: Minimum continuous heating duration after each start.
- `Maximum time to use for solving the linear programming problem in seconds`
- `Allow heating when price is below average price and given level`: This thresholding is applied after the actual optimization and minimum heating period is still taken into account by opting to not enable too short periods.
- `Allow heating when price is below given level`: This thresholding is applied after the actual optimization and minimum heating period is still taken into account by opting to not enable too short periods.
- `Result item`: Item where the optimization result is stored to.

#### Problem formulation

The mixed-integer linear program is solved over $n$ available time steps. Let

$$
x_t \in \{0,1\} \quad\text{and}\quad y_t \in \{0,1\},
\qquad t = 0,\ldots,n-1,
$$

where $x_t$ is the heating state and $y_t$ indicates that a heating cycle starts at time step $t$. The spot price at time $t$ is $p_t$.

The objective is to minimize the cost of the scheduled heating:

$$
\min \sum_{t=0}^{n-1} p_t x_t
$$

subject to the following constraints.

##### Heating demand

At least the total required amount of heating must be scheduled:

$$
C = \sum_{t=0}^{n-1} x_t \ge H.
$$

When the optimization covers two periods, the demand is also split at $T$, the length of the first period:

$$
\sum_{t=0}^{T-1} x_t \ge H_1,
\qquad
\sum_{t=T}^{n-1} x_t \ge H_2,
\qquad H = H_1 + H_2.
$$

##### Maximum gap without heating

For a maximum allowed gap $G$, every rolling window of $G+1$ time steps must contain heating:

$$
\sum_{i=0}^{G} x_{t+i} \ge 1.
$$

The first window uses the remaining gap from the previous, already scheduled period:

$$
\sum_{i=0}^{G_0} x_i \ge 1,
$$

where $G_0$ is the size of the first gap.

##### Heating starts and minimum run length

Starts are linked to transitions from off to on:

$$
y_0 \ge x_0 \quad\text{if the previous period was not heating},
$$

and, for $t \ge 1$,

$$
y_t \ge x_t - x_{t-1}.
$$

Whenever a start occurs, heating must remain on for at least $M$ time steps:

$$
\sum_{i=0}^{M-1} x_{t+i} \ge M y_t,
\qquad t=0,\ldots,n-M.
$$

Starts are forbidden in the final $M-1$ time steps because there is not enough horizon remaining to satisfy this minimum run:

$$
y_t = 0,
\qquad t=n-M+1,\ldots,n-1.
$$

##### Start limits

If configured, the number of starts is limited independently in each period:

$$
\sum_{t=0}^{T-1} y_t \le S_1,
\qquad
\sum_{t=T}^{n-1} y_t \le S_2.
$$

##### Secondary objectives

The optimization uses lexicographic tie-breaking. After the minimum cost $C^*$ has been found, a constraint preserves that result while the number of heating starts is minimized:

$$
\sum_{t=0}^{n-1} p_t x_t \le C^*,
\qquad
\min \sum_{t=0}^{n-1} y_t.
$$

Let $S^*$ be the minimum number of starts found by this second optimization. The final optimization preserves both earlier results and minimizes the largest gap without heating $G$.

$$
\sum_{t=0}^{n-1} y_t \le S^*,
\qquad
\min G.
$$

Integer variables $g_t$ track the length of the current gap, and $G$ is an upper bound for all of them:

$$
g_t =
\begin{cases}
0 & \text{if } x_t = 1,\\
g_{t-1}+1 & \text{if } x_t = 0,
\end{cases}
\qquad
G \ge g_t \quad\text{for all }t.
$$
