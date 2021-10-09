import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import FaultsView from "./components/FaultsView";
import FaultView from "./components/FaultView";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Faults = () => {
  const classes = useStyles();
  const routeMatch = useRouteMatch();

  return (
    <div className={classes.root}>
      <Grid
        container
      >
        <Grid
          item
          xs={12}
        >
          <Switch>
            <Route exact path={`${routeMatch.url}/`}
                   component={FaultsView}/>
            <Route exact path={`${routeMatch.url}/:id`}
                   component={FaultView}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Faults;
