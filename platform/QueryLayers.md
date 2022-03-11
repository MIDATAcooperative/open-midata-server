# Predicates of query language
- _id : restrict by record id
- $or : or condition
- app : restrict by app used to create record
- clear-hidden : remove the record data from all records with 'hidden' tag but leave records in result.
- created-after : restrict by creation date
- created-before : restrict by creation date
- code : system and code pairs of records to include in result
- content : midata content types to include in result
- consent-after : restrict to consents created after given date
- consent-limit : if set process query only if not too many consents involved
- consent-type-exclude : exclude specific types of consents
- creator : restrict by creator of record
- data : restrict by a condition on the records data
- deleted : if set include deleted records in result
- export : if set flag query as export query. if set to 'pseudonymized' flag query as export query for pseudonymized data.
- flat : if set do not look into streams
- from : id of first record to return
- format : restrict by FHIR resource type
- fast-index : do not update index if index query result is not empty
- force-local : process only APS of current user
- group : content groups to include
- group-system : grouping system to use. (only 'v1' supported at the moment)
- group-exclude : content groups to exclude
- history : if set include all versions of a record in result
- history-date : date to select record version
- index : index condition to apply
- load-medium-streams : if set do not use optimized implementation for medium security streams
- limit : maximum number of records to return
- name : restrict by title of record
- owner : restrict by record owner
- participant-related : if set process only records from participants of project, not from backchannel
- quick : intern-only property that consists of a record that is the candidate for the query result
- remove-hidden : remove all records with 'hidden' tag from result
- shared-after : restrict by sharing date
- skip : number of records to skip in result
- sort : property name to sort after and 'asc' or 'desc'
- study : restrict to records from a specific project
- study-group : restrict to records from a specific group of a project
- study-related : if set process only records shared back by project
- stream : restrict to records that are part of a specific stream
- streams : if set include streams into result. if set to only then include only stream records in result
- updated-after : restrict by records last updated date
- updated-before : restrict by records last updated date
- usergroup : restrict by usergroup ( team ) that has access to records
- updatable : restrict query to consents that allow update of records
- valid-until : expiration date of consent
- version : restrict by specific record version 
- writeable : if set include only records that may be updated

# Layers

## Layer: Pagination

handled query predicates:
- from
- skip

added query predicates:
- limit

## Layer: Sort

handled query predicates:
- sort

added query predicates:

## Layer: Or

handled query predicates:

- $or

added query predicates:

## Layer: ContextRestrictions

handled query predicates:

added query predicates:

- add restrictions provided by current query context

## Layer: FormatGroups

handled query predicates:
- group
- group-system
- group-exclude
- code

added query predicates:
- content

## Layer: ProcessFilters

handled query predicates:
- index [partly]
- deleted
- history-date
- creator
- app
- name
- code
- data
- remove-hidden
- clear-hidden
- created-after
- created-before
- updated-after
- updated-before
- shared-after

added query predicates:

## Layer: Pseudonymization

handled query predicates:
- study

added query predicates:
- usergroup

## Layer: Versioning

handled query predicates:
- version
- history

added query predicates:

## Layer: UserGroups

handled query predicates:
- usergroup
- export

added query predicates:

## Layer: Prefetch

handled query predicates:
- _id

added query predicates:

## Layer: ManyUserNoRestriction

handled query predicates:

added query predicates:
- (statistics query)

## Layer: Indexes

handled query predicates:
- index
- format
- fast-index

added query predicates:
- shared-after

## Layer: AccountQuery

handled query predicates:
- force-local
- owner
- consent-limit
- consent-after
- updatable
- consent-type-exclude
- study-group
- study
- created-after
- updated-after
- shared-after
- study-related
- participant-related

added query predicates:

## Layer: ConsentRestrictions

handled query predicates:

- valid-until
- history-date

added query predicates:

## Layer: Consents

handled query predicates:

- shared-after

added query predicates:

## Layer: Streams

handled query predicates:
- load-medium-streams
- stream
- quick
- writeable
- flat
- streams

added query predicates:

## Layer: AccessPermissionSet

handled query predicates:
- _id
- format
- content
- app

added query predicates: