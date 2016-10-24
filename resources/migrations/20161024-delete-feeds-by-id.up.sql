create or replace function delete_v3_feed(rid int) returns boolean as $$
declare
  t text;
  ts text[];
begin
  ts := '{v3_0_early_vote_sites, v3_0_ballot_responses, v3_0_precinct_early_vote_sites,
          v3_0_locality_early_vote_sites, v3_0_precinct_polling_locations,
          v3_0_precinct_splits, v3_0_referendums, v3_0_custom_ballot_ballot_responses,
          v3_0_elections, v3_0_precinct_split_electoral_districts,
          v3_0_referendum_ballot_responses, v3_0_sources, v3_0_states,
          v3_0_polling_locations, v3_0_candidates, v3_0_custom_ballots,
          v3_0_election_administrations, v3_0_street_segments, v3_0_ballot_candidates,
          v3_0_contest_results, v3_0_state_early_vote_sites, v3_0_ballot_line_results,
          v3_0_election_officials, v3_0_electoral_districts,
          v3_0_precinct_split_polling_locations, v3_0_localities, v3_0_contests,
          v3_0_precincts, v3_0_ballots, v3_0_precinct_electoral_districts, statistics,
          validations}'::text[];

  foreach t in array ts
  loop
    raise notice 'Removing results_id % from %', rid, t;
    execute 'delete from ' || t::regclass || ' where results_id = $1'
    using rid;
  end loop;

  update results set election_id = null where id = rid;
  delete from election_approvals where approved_result_id = rid;
  delete from results where id = rid;
  return true;

  exception when foreign_key_violation then return false;
end;
$$ language plpgsql;


create or replace function delete_v5_feed(rid int) returns boolean as $$
declare
  t text;
  ts text[];
begin
  ts := '{v5_1_ballot_measure_contests, v5_1_ballot_measure_selections,
          v5_1_ballot_selections, v5_1_ballot_styles, v5_1_candidate_contests,
          v5_1_candidate_selections, v5_1_candidates, v5_1_contact_information,
          v5_1_contests, v5_1_departments, v5_1_election_administrations,
          v5_1_elections, v5_1_electoral_districts, v5_1_localities, v5_1_offices,
          v5_1_ordered_contests, v5_1_parties, v5_1_party_contests, v5_1_party_selections,
          v5_1_people, v5_1_polling_locations, v5_1_precincts, v5_1_retention_contests,
          v5_1_schedules, v5_1_sources, v5_1_states, v5_1_street_segments,
          v5_1_voter_services, xml_tree_validations, xml_tree_values, v5_statistics}'::text[];

  foreach t in array ts
  loop
    raise notice 'Removing results_id % from %', rid, t;
    execute 'delete from ' || t::regclass || ' where results_id = $1'
    using rid;
  end loop;

  raise notice 'Removing final result for %', rid;
  delete from results where id = rid;
  return true;

  exception when foreign_key_violation then return false;
end;
$$ language plpgsql;
